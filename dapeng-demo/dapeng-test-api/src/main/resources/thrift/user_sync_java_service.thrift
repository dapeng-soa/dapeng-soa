namespace java com.isuwang.soa.user.service

include "user_domain.thrift"

service UserService {

    void createUser(user_domain.User user)

    user_domain.User getUserById(1: i32 userId)
}

