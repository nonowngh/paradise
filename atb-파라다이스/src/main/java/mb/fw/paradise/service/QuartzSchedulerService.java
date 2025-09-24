package mb.fw.paradise.service;

import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.config.RegisterModuleConfig;
import mb.fw.paradise.constants.APIContextPathConstants;
import mb.fw.paradise.service.job.DynamicQuartzJob;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class QuartzSchedulerService {

	private Scheduler scheduler;
	private WebClient interfaceInfoWebClient;
	private final RegisterModuleConfig config;

	public QuartzSchedulerService(Scheduler scheduler,
			@Qualifier("interfaceInfoWebClient") WebClient interfaceInfoWebClient, RegisterModuleConfig config) {
		this.scheduler = scheduler;
		this.interfaceInfoWebClient = interfaceInfoWebClient;
		this.config = config;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void scheduleJobsFromAPI() {
		Mono<List<InterfaceInfo>> cronScheduleInfoList = interfaceInfoWebClient.post()
				.uri(APIContextPathConstants.INTERFACE_INFO_API_SCHEDULE_LIST)
				.bodyValue(config.getRegisterProp().getInterfaceList()).retrieve()
				.bodyToMono(new ParameterizedTypeReference<List<InterfaceInfo>>() {
				});

		String taskName = config.getRegisterProp().getBatchTask();
		cronScheduleInfoList.flatMapMany(Flux::fromIterable).doOnNext(info -> {
			log.info("Register cron schedule info [{}] -> {}", info.getInterfaceId(), info.getCronExpression());
			scheduleJob(taskName, info.getInterfaceId(), info.getCronExpression());
		}).subscribe();
	}

	private void scheduleJob(String taskName, String interfaceId, String cronExpression) {
		try {
			JobDetail jobDetail = JobBuilder.newJob(DynamicQuartzJob.class).withIdentity(interfaceId)
					.usingJobData("taskName", taskName).usingJobData("interfaceId", interfaceId).storeDurably().build();

			Trigger trigger = TriggerBuilder.newTrigger().withIdentity(interfaceId + "-trigger")
					.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).forJob(jobDetail).build();

			if (!scheduler.checkExists(jobDetail.getKey())) {
				scheduler.scheduleJob(jobDetail, trigger);
			} else {
				scheduler.rescheduleJob(trigger.getKey(), trigger);
			}

		} catch (NoSuchBeanDefinitionException nsbd) {
			throw new RuntimeException(nsbd.getMessage());
		} catch (SchedulerException e) {
			throw new RuntimeException("Failed to schedule job: " + interfaceId, e);
		}

	}
}
