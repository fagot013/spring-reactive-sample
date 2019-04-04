package com.alexm.reactivesample;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ReactiveSampleApplicationTests {
	@Autowired
	ReactiveSampleApplication application;

	@Test
	public void serialEndpoint() {
		assertNotNull(application.serial().block().getCounts());
	}


	@Test
	public void parallelEndpoint() {
		assertNotNull(application.parallel().block().getCounts());
	}


	@Test
	public void nettyEndpoint() {
		assertNotNull(application.netty().block().getCounts());
	}

	@Test
	public void contextLoads() {
	}

}
