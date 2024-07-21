package com.newtelegrambot.devyNewBot.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.glassfish.grizzly.http.util.TimeStamp;

@Data
@Table(name = "customers")
@Entity
public class User {

	//Поля таблицы.
	@Id
	private Long chatId;

	private String firstname;
	private String lastname;
	private String username;
	private long registerAt;


	//Переопределение метода toString().
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