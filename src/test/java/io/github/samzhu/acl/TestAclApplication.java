package io.github.samzhu.acl;

import org.springframework.boot.SpringApplication;

public class TestAclApplication {

	public static void main(String[] args) {
		SpringApplication.from(AclApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
