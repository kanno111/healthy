package com.healthy.identity.service;

import com.healthy.identity.dto.LoginDTO;
import com.healthy.identity.dto.RegisterDTO;
import com.healthy.identity.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    void logout(String token);
}
