package com.healthy.appointment.service;

import com.healthy.appointment.dto.LoginDTO;
import com.healthy.appointment.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);

    void logout(String token);
}
