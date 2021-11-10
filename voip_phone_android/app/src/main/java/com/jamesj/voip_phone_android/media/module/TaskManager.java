package com.jamesj.voip_phone_android.media.module;

import com.jamesj.voip_phone_android.media.module.base.TaskUnit;
import com.orhanobut.logger.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author jamesj
 * @class public class TaskManager
 * @brief Task Manager
 */
public class TaskManager {

    private static TaskManager taskManager = null;

    private final ScheduledThreadPoolExecutor executor;

    private final Map<String, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();

    ////////////////////////////////////////////////////////////////////////////////

    public TaskManager( ) {
        executor = new ScheduledThreadPoolExecutor(100);
    }

    public static TaskManager getInstance ( ) {
        if (taskManager == null) {
            taskManager = new TaskManager();
        }

        return taskManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void stop ( ) {
        for (ScheduledFuture<?> scheduledFuture : taskMap.values()) {
            scheduledFuture.cancel(true);
        }

        executor.shutdown();
        Logger.d("Interval Task Manager ends.");
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public void addJob (String name, TaskUnit runner)
     * @brief TaskManager 에 새로운 Task 를 등록하는 함수
     */
    public void addTask (String name, TaskUnit taskUnit) {
        if (taskMap.get(name) != null) {
            Logger.w("TaskManager: Hashmap Key duplication error.");
            return;
        }

        ScheduledFuture<?> scheduledFuture = null;
        try {
            scheduledFuture = executor.scheduleAtFixedRate(
                    taskUnit,
                    taskUnit.getInterval(),
                    taskUnit.getInterval(),
                    TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            Logger.w("TaskManager.addTask.Exception", e);
        }

        Logger.d("Task [%s] is added. (interval=%s)", name, taskUnit.getInterval());
        taskMap.put(name, scheduledFuture);
    }

    public ScheduledFuture<?> findTask (String name) {
        if (taskMap.isEmpty()) {
            return null;
        }

        return taskMap.get(name);
    }

    public void removeTask (String name) {
        if (taskMap.isEmpty()) {
            return;
        }

        ScheduledFuture<?> scheduledFuture = findTask(name);
        if (scheduledFuture == null) {
            return;
        }

        try {
            scheduledFuture.cancel(true);
            taskMap.remove(name);
            Logger.d("Task [%s] is removed.", name);
        } catch (Exception e) {
            Logger.w("TaskManager.removeTask.Exception", e);
        }
    }

}
