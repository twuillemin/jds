package net.wuillemin.jds.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2


/**
 * The configuration of Swagger
 */
@Configuration
@EnableSwagger2
class SwaggerConfig {

    /**
     * The Bean declaring the Swagger configuration
     */
    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("net.wuillemin.jds"))
            .paths(PathSelectors.any())
            .build()
            .apiInfo(metaData())
    }

    private fun metaData(): ApiInfo {
        return ApiInfoBuilder()
            .title("JSON Data Server")
            .description("Back End API for the JSON Data Server")
            .version("0.0.1")
            .license("Copyright © 2019 Thomas Wuillemin")
            .contact(Contact("Thomas Wuillemin", "https://www.wuillemin.net/", "thomas.wuillemin@gmail.com"))
            .build()
    }
}
