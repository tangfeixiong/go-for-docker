package io.stackdocker.iscc.flagserver.dispatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.concurrent.ScheduledFuture;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Component;

import cn.com.isc.entity.Config;
import cn.com.isc.entity.Flag;
import cn.com.isc.entity.Token;
import cn.com.isc.server.ConfigService;
import cn.com.isc.server.FlagService;
import cn.com.isc.server.TokenService;

import io.stackdocker.iscc.flagserver.api.RefreshReqResp;
import io.stackdocker.iscc.flagserver.domain.RefreshConfig;

@Component
public class RefreshScheduler {
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private CronTrigger cronTrigger;

    @Autowired
    private PeriodicTrigger periodicTrigger;

    private ScheduledFuture<?> cronJob;
    
    @PostConstruct
    public void start() {
//        taskScheduler.schedule(new RunnableTask("Current Date"), new Date());
//        taskScheduler.scheduleWithFixedDelay(new RunnableTask("Fixed 1 second Delay"), 1000);
//        taskScheduler.scheduleWithFixedDelay(new RunnableTask("Current Date Fixed 1 second Delay"), new Date(), 1000);
//        taskScheduler.scheduleAtFixedRate(new RunnableTask("Fixed Rate of 2 seconds"), new Date(), 2000);
//        taskScheduler.scheduleAtFixedRate(new RunnableTask("Fixed Rate of 2 seconds"), 2000);
        cronJob = taskScheduler.schedule(new RunnableTask("Cron Trigger"), cronTrigger);
//        taskScheduler.schedule(new RunnableTask("Periodic Trigger"), periodicTrigger);
    }

	@PreDestroy 
	public void stop() {
        if (cronJob != null) cronJob.cancel(false);
//      taskScheduler.initialize();
//      ScheduledExecutorService scheduledExecutorService = threadPoolTaskScheduler.getScheduledExecutor();
//      Threads.normalShutdown(scheduledExecutorService, shutdownTimeout, TimeUnit.SECONDS);
	} 
    
    private String mountRoot = "/tmp/mnt-home";	
    private Map<String, Work> works = new HashMap<String, Work>();   
    private int minusSeconds = 0;
	
	@Autowired
	private ConfigService configService;
    
	@Autowired
	private FlagService flagService;
    
    public RefreshScheduler() {
        String env = System.getenv("VOLUME_MOUNT");
        if ( env != null && env.trim().length() > 0) mountRoot = env;   
        env = System.getenv("MINUS_SECONDS");
        if ( env != null && env.trim().length() > 0) {
            try {
                minusSeconds = Integer.parseInt(env);
            } catch (Exception ex) {
                ex.printStackTrace();
                minusSeconds = 0;
            }
        }     
    }

    public RefreshReqResp create(RefreshReqResp req) {
        RefreshReqResp resp = new RefreshReqResp();
        Path mnt = Paths.get(mountRoot);
        if (Files.notExists(mnt)) {
            resp.setStateCode(new Integer(10));
    		resp.setStateMessage("Data store mount point not exists");
            return resp;            
        }
        
        if ((req.getId() == null || req.getId() <= 0) && (req.getName() == null || req.getName().trim() == "")) {
			resp.setStateCode(new Integer(100));
			resp.setStateMessage("JSON id or name is required");
			return resp;			            
        }
        if (req.getId() != null && req.getId() > 0) {
            resp.setId(req.getId());
        }
        if (req.getName() != null && req.getName().trim() != "") {
            resp.setName(req.getName().trim());
        } else {
            resp.setName(req.getId().toString());
        }
        
        if (works.containsKey(resp.getName())) {
			resp.setStateCode(new Integer(11));
			resp.setStateMessage("Refresh work has already created, delete first");
			return resp;			                        
        }
        
        if (req.getImageId() == null || req.getImageId() <= 0) {
            resp.setStateCode(new Integer(101));
			resp.setStateMessage("Image id is required");
            return resp;                        
        }
        resp.setImageId(req.getImageId());
        if (req.getProjectId() != null && req.getProjectId() > 0) {
            resp.setProjectId(req.getProjectId());
        }
        if (req.getPeriodic() != null) {
            resp.setPeriodic(req.getPeriodic());
        }
        if (req.getRefreshingAt() != null) {
            resp.setRefreshingAt(req.getRefreshingAt());
        }
        if (req.getRounds() != null && req.getRounds() > 0) {
            resp.setRounds(req.getRounds());
        }
        if (req.getCount() != null && req.getCount() > 0) {
            resp.setCount(req.getCount());
        }
        if (req.getDataStore() != null && req.getDataStore().trim() != "") {
            resp.setDataStore(req.getDataStore().trim());
        }
		if ( req.getInfo() == null || req.getInfo().size() == 0 ) {
			resp.setStateCode(new Integer(102));
			resp.setStateMessage("Refresh settings were required");
			return resp;			
		}
        resp.setConfig(req.getConfig());

        if (minusSeconds > 0) resp.setMinusSeconds(minusSeconds);
		for (Map.Entry<String, RefreshConfig> entry : req.getInfo().entrySet()) {
            String key = entry.getKey();
            RefreshConfig value = entry.getValue();
	        System.out.println("Schedule Key : " + key + " Value : " + value);
            
            RefreshConfig item = new RefreshConfig();
            if (value.getContainerId() == null || value.getContainerId() <= 0) {
                resp.setStateCode(new Integer(103));
			    resp.setStateMessage("Container id is required");
                return resp;                
            } 
            item.setContainerId(value.getContainerId());
            if (value.getName() != null && item.getName().trim() != "")
                item.setName(value.getName().trim());
            else 
                item.setName(resp.getId().toString());
			if ( value.getTeamId() == null || value.getTeamId() <= 0 ){
                resp.setStateCode(new Integer(104));
			    resp.setStateMessage("Team id is required");
                return resp;
			}
            item.setTeamId(value.getTeamId());
            if (value.getSubPath() != null && value.getSubPath().trim() != "")
                item.setSubPath(value.getSubPath().trim());
            else 
                item.setSubPath(item.getContainerId().toString());
                        
            Path dir = mnt.resolve(resp.getDataStore()).resolve(item.getSubPath());
            if (Files.notExists(dir)) {
                try {
                   dir.toFile().mkdirs();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    resp.setStateCode(new Integer(12));
		            resp.setStateMessage("Failed to create data store, error: " + ex.getMessage());
                    return resp;
                }
            }
            
            resp.addRefreshConfig(key, item);
        }
        
        if (resp.getPeriodic() == 0) return resp;
        if (new GregorianCalendar(1999, 11, 30).getTime().compareTo(resp.getRefreshingDatetime()) < 0
                &&  resp.getRefreshingDatetime().getTime() - System.currentTimeMillis() < 1000L * resp.getMinusSeconds()) {
            resp.setStateCode(new Integer(105));
            resp.setStateMessage("Could not count to minus " + resp.getMinusSeconds() + " seconds");
            return resp;                    
        }
        
        long delay = 1000L * resp.getPeriodic();
        // PeriodicTrigger trigger = new PeriodicTrigger(delay, TimeUnit.MILLISECONDS);
        // trigger.setFixedRate(true);
        // trigger.setInitialDelay(resp.getRefreshingDatetime().getTime() - System.currentTimeMillis());
        // taskScheduler.schedule(new RunnableTask(this, resp.getName()), periodicTrigger);
        Runnable task = new RunnableTask(this, resp.getName());
        Date startTime = resp.getRefreshingDatetime();
        try {
            ScheduledFuture<?> job = taskScheduler.scheduleAtFixedRate(task, startTime, delay);
            works.put(resp.getName(), new Work(resp, job));
        } catch (Exception ex) {
            ex.printStackTrace();
            resp.setStateCode(new Integer(13));
            resp.setStateMessage("Could not create scheduler, error: " + ex.getMessage());            
        } 
        return resp;
    }
    
    public RefreshReqResp update(RefreshReqResp req) {
        RefreshReqResp resp = delete(req);
        if (resp.getStateCode() != 0) {
            return resp;
        }
        return create(req);
    }
    
    public RefreshReqResp delete(RefreshReqResp req) {
        RefreshReqResp resp = new RefreshReqResp();

        if ((req.getId() == null || req.getId() <= 0) && (req.getName() == null || req.getName().trim() == "")) {
			resp.setStateCode(new Integer(100));
			resp.setStateMessage("JSON id or name is required");
			return resp;			            
        }
        if (req.getId() != null && req.getId() > 0) {
            resp.setId(req.getId());
        }
        if (req.getName() != null && req.getName().trim() != "") {
            resp.setName(req.getName().trim());
        } else {
            resp.setName(req.getId().toString());
        }
        
        if (!works.containsKey(resp.getName())) {
			resp.setStateCode(new Integer(20));
			resp.setStateMessage("Refreshing work does not exists");
			return resp;			                        
        }

		Work work = works.remove(resp.getName());
        work.getJob().cancel(false);
        return work.getContext();
    }

    RefreshReqResp todo(String message) {
        RefreshReqResp resp = works.get(message).getContext();
        resp.incrementsCount();
        if (resp.getCount() > resp.getRounds()) {
	        System.out.println("Game over, bout : " + resp.getRounds());
            works.remove(resp.getName()).getJob().cancel(false);
            return resp;
        } 
		for (Map.Entry<String, RefreshConfig> entry : resp.getInfo().entrySet()) {
            String key = entry.getKey();
            RefreshConfig value = entry.getValue();

		    System.out.println("get flag with: env=" + resp.getImageId() + " teamNo=" + value.getTeamId() + " round=" + resp.getCount());
			Flag result = flagService.get( resp.getImageId(), value.getTeamId(), resp.getCount() );
			if ( result == null ){
                resp.setStateCode(new Integer(200));
			    resp.setStateMessage("Could not find flag value");
                return resp;                
            }
		    System.out.println("flag: " + result);
			
            Path fp = Paths.get(mountRoot, resp.getDataStore(), value.getSubPath(), value.getName());
            try {
               Files.write(fp, result.getMd5String().getBytes());
            } catch (Exception ex) {
                ex.printStackTrace();
                resp.setStateCode(new Integer(14));
	            resp.setStateMessage("Failed to write bytes into destination, error: " + ex.getMessage());
                works.remove(resp.getName()).getJob().cancel(false);
                return resp;
            }               
			
            value.setFlag(result);
		}
        
        return resp;
    }
    
}

class RunnableTask implements Runnable {
    private String message;
    private RefreshScheduler scheduler;

    public RunnableTask(String message) {
        this.message = message;
    }

    public RunnableTask(RefreshScheduler scheduler, String message) {
        this(message);
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        System.out.println("Runnable Task with " + message + " on thread " + Thread.currentThread().getName());
        if ( scheduler != null ) scheduler.todo(message);
    }
}

class Work {
    private RefreshReqResp context;
    private ScheduledFuture<?> job;
    public Work(RefreshReqResp context, ScheduledFuture<?> job) {
        this.context = context;
        this.job = job;
    }
    
    public RefreshReqResp getContext() {
        return context;
    }
    
    public ScheduledFuture<?> getJob() {
        return job;
    }
}


