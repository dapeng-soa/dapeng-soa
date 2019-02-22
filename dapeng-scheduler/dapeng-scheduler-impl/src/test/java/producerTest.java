import com.github.dapeng.scheduler.events.TaskEvent;
import com.today.api.scheduler.enums.TaskStatusEnum;
import com.today.kafka.TaskMsgKafkaProducer;

import java.util.UUID;

/**
 * @author huyj
 * @Created 2019-02-18 16:05
 */
public class producerTest {
    public static void main(String[] arg0) throws Exception{

        String tranID ="dapeng-task-"+UUID.randomUUID().toString();
       TaskMsgKafkaProducer taskMsgKafkaProducer = new TaskMsgKafkaProducer("192.168.5.96:9092").withValueByteArraySerializer().createProducerWithTran(tranID);
        //TaskMsgKafkaProducer taskMsgKafkaProducer = new TaskMsgKafkaProducer("192.168.5.96:9092").withValueStringSerializer().createProducerWithTran(tranID);
//        TaskMsgKafkaProducer taskMsgKafkaProducer = new TaskMsgKafkaProducer("192.168.5.96:9092").createProducer();

        for (Long i = 0L; i < 10; i++) {
//            taskMsgKafkaProducer.sendMsg("Task-test","test-message-info-"+i);
            //taskMsgKafkaProducer.sendMsgByTransaction("Task-test",i,("test-message-info-"+i).getBytes("UTF-8"));
           // taskMsgKafkaProducer.sendMsgByTransaction("Task-test",i,("test-message-info-"+i));

            TaskEvent taskEvent = new TaskEvent();
            taskEvent.id(System.currentTimeMillis());
            taskEvent.setServiceName("serviceName-test");
            taskEvent.setMethodName("methodName--test");
            taskEvent.setVersion("versionName-test");
            taskEvent.setCostTime(12);
            taskEvent.setTaskStatus(TaskStatusEnum.SUCCEED);

            //发布消息
            //CommonEventBus.fireEvent(taskEvent);
            taskMsgKafkaProducer.sendTaskMessage("dapeng-task", taskEvent);

        }

    }
}
