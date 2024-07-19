package com.newtelegrambot.devyNewBot.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import org.glassfish.grizzly.http.util.TimeStamp;

@Data
@Entity
public class User {

	@Id
	private Long chatId;

	private String firstname;
	private String lastname;
	private String username;
	private TimeStamp registerAt;


	@Override
	public String toString() {
		return "User{" +
				"chatId=" + chatId +
				", firstname='" + firstname + '\'' +
				", lastname='" + lastname + '\'' +
				", username='" + username + '\'' +
				", registerAt=" + registerAt +
				'}';
	}
}