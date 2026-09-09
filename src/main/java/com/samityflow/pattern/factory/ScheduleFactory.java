package com.samityflow.pattern.factory;

import com.samityflow.pattern.schedule.GracePeriodSchedule;
import com.samityflow.pattern.schedule.Schedule;
import com.samityflow.pattern.schedule.SeasonalSchedule;
import com.samityflow.pattern.schedule.StandardWeeklySchedule;


public class ScheduleFactory {


    public Schedule create(String scheduleType) {


        if (scheduleType == null) {
            throw new IllegalArgumentException(
                    "Schedule type cannot be null"
            );
        }


        return switch (scheduleType.toUpperCase()) {


            case "STANDARD_WEEKLY",
                 "WEEKLY",
                 "STANDARD" ->
                    new StandardWeeklySchedule();


            case "GRACE_PERIOD",
                 "GRACE" ->
                    new GracePeriodSchedule(2);


            case "SEASONAL",
                 "QUARTERLY" ->
                    new SeasonalSchedule(3);


            default ->
                    throw new IllegalArgumentException(
                            "Unknown schedule type: "
                                    + scheduleType
                    );
        };
    }
}