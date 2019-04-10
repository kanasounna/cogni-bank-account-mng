package com.cognibank.accountmanagment.cognibankaccountmanagment;

import com.cognibank.accountmanagment.cognibankaccountmanagment.Services.RabbitMQService;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CogniBankAccountManagmentApplication {

	@Autowired
	private RabbitMQService rabbitMQService;

	@Autowired
	ConnectionFactory connectionFactory;

	@Bean
	public void createQueue(){
		rabbitMQService.binding(rabbitMQService.queue(),rabbitMQService.exchange());
		rabbitMQService.container(connectionFactory);
	}

	public static void main(String[] args) {
		SpringApplication.run(CogniBankAccountManagmentApplication.class, args);
	}

}

