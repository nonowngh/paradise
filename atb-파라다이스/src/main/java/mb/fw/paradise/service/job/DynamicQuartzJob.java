package mb.fw.paradise.service.job;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import mb.fw.paradise.module.BatchModule;

public class DynamicQuartzJob implements Job {

	@Autowired
	private ApplicationContext applicationContext;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException, NoSuchBeanDefinitionException {
		JobDataMap dataMap = context.getMergedJobDataMap();
		String interfaceId = dataMap.getString("interfaceId");
		String taskName = dataMap.getString("taskName");
		BatchModule task = applicationContext.getBean(taskName, BatchModule.class);
		task.executeTask(interfaceId);
	}
}
