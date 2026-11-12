package com.yep.service;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.ActivityLevel;
import com.yep.dto.LearningDtos.ActivityCompletionSummary;

/** Selects the correct completion workflow (Strategy selection) for the activity level. */
@Service
public class ActivityCompletionService {

    private final ActivityService activityService;
    private final Map<ActivityLevel, ActivityCompletionWorkflow> workflows = new EnumMap<>(ActivityLevel.class);

    public ActivityCompletionService(ActivityService activityService, LevelOneCompletionWorkflow levelOne,
            LevelTwoCompletionWorkflow levelTwo) {
        this.activityService = activityService;
        workflows.put(ActivityLevel.ONE, levelOne);
        workflows.put(ActivityLevel.TWO, levelTwo);
    }

    @Transactional
    public ActivityCompletionSummary complete(Long activityId) {
        LearningActivity activity = activityService.find(activityId);
        return workflows.get(activity.getLevel()).complete(activityId);
    }
}
