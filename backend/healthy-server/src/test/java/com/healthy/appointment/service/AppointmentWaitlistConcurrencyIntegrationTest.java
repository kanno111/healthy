package com.healthy.appointment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.healthy.appointment.entity.AppointmentWaitlist;
import com.healthy.appointment.entity.DoctorScheduleSlot;
import com.healthy.appointment.mapper.AppointmentWaitlistMapper;
import com.healthy.appointment.mapper.DoctorScheduleSlotMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_CONFIRMED;
import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_EXPIRED;
import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_OFFERED;
import static com.healthy.appointment.constant.Constant.WAITLIST_STATUS_WAITING;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AppointmentWaitlistConcurrencyIntegrationTest {
    @Autowired
    private AppointmentWaitlistMapper appointmentWaitlistMapper;
    @Autowired
    private DoctorScheduleSlotMapper doctorScheduleSlotMapper;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testSlotId;
    private long patientSequence;

    @AfterEach
    void cleanUp() {
        if (testSlotId != null) {
            appointmentWaitlistMapper.delete(new LambdaQueryWrapper<AppointmentWaitlist>()
                    .eq(AppointmentWaitlist::getScheduleSlotId, testSlotId));
            jdbcTemplate.update("DELETE FROM doctor_schedule_slot WHERE id = ?", testSlotId);
        }
    }

    @Test
    void concurrentForUpdateAllocationsChooseDifferentPatientsInFifoOrder() throws Exception {
        beginScenario();
        Long firstId = insertWaitlist(WAITLIST_STATUS_WAITING, null);
        Long secondId = insertWaitlist(WAITLIST_STATUS_WAITING, null);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch firstRowLocked = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Long> first = executor.submit(() -> transactionTemplate.execute(status -> {
                doctorScheduleSlotMapper.findByIdForUpdate(testSlotId);
                AppointmentWaitlist waiting = appointmentWaitlistMapper.selectFirstWaitingForUpdate(testSlotId);
                firstRowLocked.countDown();
                await(releaseFirstTransaction);
                markOffered(waiting.getId());
                return waiting.getId();
            }));
            assertThat(firstRowLocked.await(2, TimeUnit.SECONDS)).isTrue();

            Future<Long> second = executor.submit(() -> transactionTemplate.execute(status -> {
                doctorScheduleSlotMapper.findByIdForUpdate(testSlotId);
                AppointmentWaitlist waiting = appointmentWaitlistMapper.selectFirstWaitingForUpdate(testSlotId);
                markOffered(waiting.getId());
                return waiting.getId();
            }));

            releaseFirstTransaction.countDown();
            assertThat(first.get(3, TimeUnit.SECONDS)).isEqualTo(firstId);
            assertThat(second.get(3, TimeUnit.SECONDS)).isEqualTo(secondId);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void confirmAndExpiryConditionalUpdatesHaveExactlyOneWinner() throws Exception {
        beginScenario();
        LocalDateTime expireTime = LocalDateTime.now().plusHours(1);
        Long waitlistId = insertWaitlist(WAITLIST_STATUS_OFFERED, expireTime);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> confirm = executor.submit(() -> {
                await(start);
                return appointmentWaitlistMapper.update(null, new LambdaUpdateWrapper<AppointmentWaitlist>()
                        .eq(AppointmentWaitlist::getId, waitlistId)
                        .eq(AppointmentWaitlist::getStatus, WAITLIST_STATUS_OFFERED)
                        .gt(AppointmentWaitlist::getOfferExpireTime, expireTime.minusSeconds(1))
                        .set(AppointmentWaitlist::getStatus, WAITLIST_STATUS_CONFIRMED));
            });
            Future<Integer> expire = executor.submit(() -> {
                await(start);
                return appointmentWaitlistMapper.update(null, new LambdaUpdateWrapper<AppointmentWaitlist>()
                        .eq(AppointmentWaitlist::getId, waitlistId)
                        .eq(AppointmentWaitlist::getStatus, WAITLIST_STATUS_OFFERED)
                        .le(AppointmentWaitlist::getOfferExpireTime, expireTime.plusSeconds(1))
                        .set(AppointmentWaitlist::getStatus, WAITLIST_STATUS_EXPIRED));
            });

            start.countDown();
            assertThat(confirm.get(3, TimeUnit.SECONDS) + expire.get(3, TimeUnit.SECONDS)).isEqualTo(1);
            AppointmentWaitlist finalRecord = appointmentWaitlistMapper.selectById(waitlistId);
            assertThat(finalRecord.getStatus()).isIn(WAITLIST_STATUS_CONFIRMED, WAITLIST_STATUS_EXPIRED);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void repeatedFifoSelectionReturnsWaitingPatientsInCreationOrder() {
        beginScenario();
        Long firstId = insertWaitlist(WAITLIST_STATUS_WAITING, null);
        Long secondId = insertWaitlist(WAITLIST_STATUS_WAITING, null);
        Long thirdId = insertWaitlist(WAITLIST_STATUS_WAITING, null);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        List<Long> allocatedIds = List.of(
                allocateFirstWaiting(transactionTemplate),
                allocateFirstWaiting(transactionTemplate),
                allocateFirstWaiting(transactionTemplate));

        assertThat(allocatedIds).containsExactly(firstId, secondId, thirdId);
    }

    private Long allocateFirstWaiting(TransactionTemplate transactionTemplate) {
        return transactionTemplate.execute(status -> {
            doctorScheduleSlotMapper.findByIdForUpdate(testSlotId);
            AppointmentWaitlist waiting = appointmentWaitlistMapper.selectFirstWaitingForUpdate(testSlotId);
            markOffered(waiting.getId());
            return waiting.getId();
        });
    }

    private void markOffered(Long waitlistId) {
        assertThat(appointmentWaitlistMapper.update(null, new LambdaUpdateWrapper<AppointmentWaitlist>()
                .eq(AppointmentWaitlist::getId, waitlistId)
                .eq(AppointmentWaitlist::getStatus, WAITLIST_STATUS_WAITING)
                .set(AppointmentWaitlist::getStatus, WAITLIST_STATUS_OFFERED)
                .set(AppointmentWaitlist::getOfferExpireTime, LocalDateTime.now().plusMinutes(10)))).isEqualTo(1);
    }

    private Long insertWaitlist(String status, LocalDateTime offerExpireTime) {
        AppointmentWaitlist waitlist = new AppointmentWaitlist();
        waitlist.setPatientId(testSlotId + 10_000L + ++patientSequence);
        waitlist.setScheduleSlotId(testSlotId);
        waitlist.setStatus(status);
        waitlist.setOfferExpireTime(offerExpireTime);
        assertThat(appointmentWaitlistMapper.insert(waitlist)).isEqualTo(1);
        return waitlist.getId();
    }

    private void beginScenario() {
        DoctorScheduleSlot slot = new DoctorScheduleSlot();
        slot.setDoctorId(8_000_000_000L + Math.floorMod(System.nanoTime(), 100_000_000L));
        slot.setScheduleDate(LocalDate.of(2099, 1, 1));
        slot.setSessionType("OTHER");
        slot.setSessionName("test");
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(9, 0));
        slot.setAverageConsultationMinutes(10);
        slot.setTotalCapacity(10);
        slot.setRemainingCapacity(0);
        slot.setNextQueueNumber(1);
        slot.setStatus("CLOSED");
        slot.setVersion(0);
        assertThat(doctorScheduleSlotMapper.batchInsert(List.of(slot))).isEqualTo(1);
        testSlotId = slot.getId();
        patientSequence = 0;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent test worker");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent test worker was interrupted", exception);
        }
    }
}
