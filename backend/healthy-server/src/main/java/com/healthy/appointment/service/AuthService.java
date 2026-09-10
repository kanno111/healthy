package com.healthy.appointment.service;

import com.healthy.appointment.dto.LoginDTO;
import com.healthy.appointment.dto.RegisterDTO;
import com.healthy.appointment.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    void logout(String token);
}
