package com.example.hello;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HelloControllerTest {
    @Test
    void helloShouldReturnText() {
        var c = new HelloController();
        assertThat(c.hello()).isEqualTo("hello world");
    }
}
