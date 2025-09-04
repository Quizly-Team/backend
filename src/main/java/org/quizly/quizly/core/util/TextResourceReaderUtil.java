package org.quizly.quizly.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
@RequiredArgsConstructor
public class TextResourceReaderUtil {

  public String load(String path) {
    try {
      ClassPathResource resource = new ClassPathResource(path);
      InputStream inputStream = resource.getInputStream();
      return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
  }
}