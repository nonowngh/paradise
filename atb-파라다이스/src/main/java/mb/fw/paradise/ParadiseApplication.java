package mb.fw.paradise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.mb.indigo2.springsupport.AdaptorConfig;

import lombok.extern.slf4j.Slf4j;
import mb.fw.adaptor.util.AdaptorStarter;
import mb.fw.atb.util.MDCLogging;

@ImportResource({ "classpath:bean.xml" })
@EnableAsync
@EnableScheduling
@SpringBootApplication
@Slf4j
@ComponentScan(basePackages = { "mb.fw.paradise.common", "mb.fw.adaptor" })
public class ParadiseApplication {
	public static void main(String[] args) throws Exception {
		System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
				"com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
		AdaptorStarter.init();
		String adaptorName = AdaptorConfig.getInstance().getAdaptorName();
		log.info("adaptorName: {}", adaptorName);
		MDCLogging.create("NONE", "NONE", adaptorName);
		SpringApplication.run(ParadiseApplication.class, args);
	}
}
