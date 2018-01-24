package com.isuwang.soa.user.scala

        import com.github.dapeng.core._;
        import com.github.dapeng.org.apache.thrift._;
        import java.util.ServiceLoader;
        import com.isuwang.soa.user.scala.UserServiceCodec._;
        import com.isuwang.soa.user.scala.service.UserService;

        /**
         * Autogenerated by Dapeng-Code-Generator (1.2.2)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated

        **/
        class UserServiceClient extends UserService {

        import java.util.function.{ Function ⇒ JFunction, Predicate ⇒ JPredicate, BiPredicate }
          implicit def toJavaFunction[A, B](f: Function1[A, B]) = new JFunction[A, B] {
          override def apply(a: A): B = f(a)
        }

          val serviceName = "com.isuwang.soa.user.service.UserService"
          val version = "1.0.0"
          val pool = {
            val serviceLoader = ServiceLoader.load(classOf[SoaConnectionPoolFactory])
          if (serviceLoader.iterator().hasNext) {
          val poolImpl = serviceLoader.iterator().next().getPool
          poolImpl.registerClientInfo(serviceName,version)
          poolImpl
          } else null
           }

        def getServiceMetadata: String = {
        pool.send(
          serviceName,
          version,
          "getServiceMetadata",
          new getServiceMetadata_args,
          new GetServiceMetadata_argsSerializer,
          new GetServiceMetadata_resultSerializer
        ).success
        }


        
             /**
             * 
             **/
            def createUser(user:com.isuwang.soa.user.scala.domain.User ) : Unit = {

              val response = pool.send(
              serviceName,
              version,
              "createUser",
              createUser_args(user),
              new CreateUser_argsSerializer(),
              new CreateUser_resultSerializer())

              

            }
          
             /**
             * 
             **/
            def getUserById(userId:Int ) : com.isuwang.soa.user.scala.domain.User = {

              val response = pool.send(
              serviceName,
              version,
              "getUserById",
              getUserById_args(userId),
              new GetUserById_argsSerializer(),
              new GetUserById_resultSerializer())

              response.success

            }
          
      }
      