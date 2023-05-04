package commu.unhaha.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.add}")
    private String fileAdd;

    @Value("${file.fileArticle}")
    private String fileArticle;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/images/userImage/**")
                .addResourceLocations(fileAdd);
        registry.addResourceHandler("/images/article/**")
                .addResourceLocations(fileArticle);
    }

}
