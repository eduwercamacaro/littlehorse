package io.littlehorse;

import io.littlehorse.common.model.getable.objectId.TaskRunIdModel;
import io.littlehorse.common.model.getable.objectId.TenantIdModel;
import io.littlehorse.sdk.common.proto.TaskRunId;
import io.littlehorse.sdk.common.proto.WfRunId;
import io.littlehorse.server.streams.taskqueue.OneTaskQueue;
import io.littlehorse.server.streams.taskqueue.TaskQueueManager;
import io.littlehorse.server.streams.topology.core.BackgroundContext;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.streams.processor.TaskId;

public class TaskQueueManagerMemoryAnalyzer {

    static void main() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        TaskQueueManager manager = new TaskQueueManager(null);
        OneTaskQueue q = new OneTaskQueue("test", manager, new TenantIdModel("test"));
        for (int i = 0; i < 2_000_000; i++) {
            WfRunId wfRunId =
                    WfRunId.newBuilder().setId(UUID.randomUUID().toString()).build();
            TaskRunId taskRun = TaskRunId.newBuilder()
                    .setWfRunId(wfRunId)
                    .setTaskGuid("1_2")
                    .build();
            TaskRunIdModel taskRunId = TaskRunIdModel.fromProto(taskRun, TaskRunIdModel.class, new BackgroundContext());
            q.onTaskScheduled(TaskId.parse("1_2"), taskRunId);
        }
        System.out.println("done + " + q.size());
        TimeUnit.HOURS.sleep(2);
    }
}
