package com.example.prac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PracApplication {
    public static void main(String[] args) {
        SpringApplication.run(PracApplication.class, args);
    }
}

// TODO пагинация
// TODO при выборе билета еще запрос, существует ли билет (просто getTicketById)
// TODO свои Exception-ы (как в модели)
// несуществующий город
// дата, время, датавремя не соответствуют формату
// возвращение раньше отправления
// превышено максимальное количество перелетов
