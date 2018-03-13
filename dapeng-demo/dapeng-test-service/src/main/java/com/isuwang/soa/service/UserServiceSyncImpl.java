package com.isuwang.soa.service;

import com.github.dapeng.core.SoaException;
import com.isuwang.soa.user.domain.User;
import com.isuwang.soa.user.service.UserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserServiceSyncImpl implements UserService {

    private static final Map<Integer,User> users = new HashMap<>();

    @Override
    public void createUser(User user) throws SoaException {
        System.out.println(" =============createUser==================");
        users.put(user.id, user);
    }

    @Override
    public User getUserById(Integer userId) throws SoaException {
        System.out.println(" =============getUserById==================");
        return users.get(userId);
    }
}
