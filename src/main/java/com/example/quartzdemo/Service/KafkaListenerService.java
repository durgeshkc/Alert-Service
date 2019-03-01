package com.example.quartzdemo.Service;

//import com.example.quartzdemo.controller.EmailJobSchedulerController;
import com.example.quartzdemo.Repository.UserRepository;
import com.example.quartzdemo.job.EmailJob;
import com.example.quartzdemo.model.User;
import com.example.quartzdemo.payload.ScheduleEmailRequest;
import com.example.quartzdemo.payload.ScheduleEmailResponse;
import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Service
public class KafkaListenerService {

    @Autowired(required = true)
    private UserRepository userRepository;
    @Autowired
    private Scheduler scheduler;

    public static final String ACCOUNT_SID = "AC20c22dfaa7aefee958ba5eda0b46edf5";
    public static final String AUTH_TOKEN  = "6df52a709d0cb96805763728992e2495";
    // Create a phone number in the Twilio console.................
    public static final String TWILIO_NUMBER = "+12017205671";


    //Listener for User Registration Service to store user details like username,email & phone number
    //in local mysql.
    @KafkaListener(topics = "Kafka_NewUser_Registration", groupId = "group_id")
    public void consumeUserRegData(String message) {
        System.out.println("Consumed message: " + message);

        String[] strMessage = message.split(",");
        User user = new User();

        user.setEmail(strMessage[4].split(":")[1].replace("\"",""));
        user.setUsername(strMessage[1].split(":")[1].replace("\"",""));
        System.out.println(user.getUsername());

        userRepository.save(user);

    }

    /*
        A JobDetail represents an instance of a Job.It also contains additional data
        in the form of a JobDataMap that is passed to the Job when it is executed.
     */
    private JobDetail buildJobDetail(ScheduleEmailRequest scheduleEmailRequest) {
        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put("email", scheduleEmailRequest.getEmail());
        jobDataMap.put("subject", scheduleEmailRequest.getSubject());
        jobDataMap.put("body", scheduleEmailRequest.getBody());

        return JobBuilder.newJob(EmailJob.class)
                .withIdentity(UUID.randomUUID().toString(), "email-jobs")
                .withDescription("Send Email Job")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    /*
    TRIGGERS defines the schedule at which a given Job will be executed. A Job can have many Triggers,
    but a Trigger can only be associated with one Job.
     */
    private Trigger buildJobTrigger(JobDetail jobDetail, ZonedDateTime startAt) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "email-triggers")
                .withDescription("Send Email Trigger")
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }

    //Listener for Threshold Service to get the details of metrics that have crossed their threshold
    // value and then it will send the alert to the respective user by matching username from locally s
    // tored database and getting email and phone number from their only.

    @KafkaListener(topics = "kafkaAppRegistration", groupId = "group_id")
    public void consumeThresholdData(String message) {
        System.out.println("Consumed message: " + message);
        String[] strMessage = message.split(",");
//        String usernameFromThreshold = strMessage[1].split(":")[1].replace("\"","");
//
        //User user = userRepository.getUserDetails(usernameFromThreshold);


        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        Message textMessage = Message.creator(
                new PhoneNumber("+918861458359"),
                new PhoneNumber(TWILIO_NUMBER),
                "SYSCOP Alert Emergency")
                .create();


        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        ResourceSet<Message> messages = Message.reader().read();
        for (Message message1 : messages) {
            System.out.println(message1.getSid() + " : " + message1.getStatus());
        }

        ScheduleEmailRequest scheduleEmailRequest= new ScheduleEmailRequest();



        try {

            ZonedDateTime currentdateTime = ZonedDateTime.now();        //to get current date and time.......
            ZonedDateTime dateTime = currentdateTime.plusSeconds(15);

            //scheduleEmailRequest.setEmail(user.getEmail());
            scheduleEmailRequest.setEmail("durgeshkumar0895@gmail.com");
            scheduleEmailRequest.setBody("CPU usage has increased tremendously.");
            scheduleEmailRequest.setSubject("Syscop Alert");

            JobDetail jobDetail = buildJobDetail(scheduleEmailRequest);

            Trigger trigger = buildJobTrigger(jobDetail, dateTime);
            scheduler.scheduleJob(jobDetail, trigger);

            ScheduleEmailResponse scheduleEmailResponse = new ScheduleEmailResponse(true,
                    jobDetail.getKey().getName(), jobDetail.getKey().getGroup(), "Email Scheduled Successfully!");
        } catch (SchedulerException ex) {

            ScheduleEmailResponse scheduleEmailResponse = new ScheduleEmailResponse(false,
                    "Error scheduling email. Please try later!");
        }

    }


}
