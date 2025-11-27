package mb.fw.paradise.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import mb.fw.paradise.api.model.InterfaceInfo;
import mb.fw.paradise.config.ModuleConfig;
import mb.fw.paradise.constants.ApiContextPathConstants;
import mb.fw.paradise.service.job.DynamicQuartzJob;

@Slf4j
@Service
public class QuartzSchedulerService {

	private Scheduler scheduler;
	private WebClient interfaceInfoWebClient;
	private final ModuleConfig config;

	private List<InterfaceInfo> lastCronScheduleInfoList;

	@Autowired
	private TaskScheduler taskScheduler;

	public QuartzSchedulerService(@Autowired(required = false) Scheduler scheduler,
			@Qualifier("interfaceInfoWebClient") WebClient interfaceInfoWebClient, ModuleConfig config) {
		this.scheduler = scheduler;
		this.interfaceInfoWebClient = interfaceInfoWebClient;
		this.config = config;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void scheduleJobsFromAPI() {
		if (scheduler == null)
			return;
		try {
			List<InterfaceInfo> cronScheduleInfoList = interfaceInfoWebClient.post()
					.uri(ApiContextPathConstants.INTERFACE_INFO_API_SCHEDULE_LIST).bodyValue(config.getInterfaceList())
					.retrieve().bodyToMono(new ParameterizedTypeReference<List<InterfaceInfo>>() {
					}).block(); // ← 동기 호출;

			if (cronScheduleInfoList != null && cronScheduleInfoList.equals(lastCronScheduleInfoList)) {
				log.debug("⏭️ 스케줄 정보 동일 → 갱신 패스");
				return;
			}

			String taskName = config.getBatchTask();
			if (cronScheduleInfoList != null) {
				for (InterfaceInfo info : cronScheduleInfoList) {
					log.info("Register cron schedule info [{}] -> {}", info.getInterfaceId(), info.getCronExpression());
					scheduleJob(taskName, info.getInterfaceId(), info.getCronExpression());
				}
			}

			lastCronScheduleInfoList = cronScheduleInfoList;
		} catch (Exception e) {
			log.error("🔥 스케줄 초기화 중 오류 발생 -> {}", e.getMessage());
			throw new RuntimeException("스케줄 초기화 실패", e);
		}
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

	public void refreshScheduler() {
		log.debug("🔄 인터페이스 스케줄 리프레시 실행");
		scheduleJobsFromAPI(); // API 호출해서 최신 크론 정보 반영
	}

	@PostConstruct
	public void startRefreshTask() {
		if (scheduler == null || config.getSchedulerRefreshIntervalSeconds() <= 0)
			return;
		taskScheduler.scheduleAtFixedRate(this::refreshScheduler, config.getSchedulerRefreshIntervalSeconds() * 1000L);
	}
}
