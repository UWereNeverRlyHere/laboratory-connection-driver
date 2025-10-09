package ywh.services.data.models;

import lombok.Data;
import ywh.services.data.enums.ProcessResult;
import ywh.services.data.models.api.ResultResponse;

import java.io.File;
import java.util.Optional;

@Data
public final class ProcessorResult {
    private ProcessResult result = ProcessResult.SUCCESS;
    private Optional<File> file = Optional.empty();
    private Optional<ResultResponse> response = Optional.empty();

    public ProcessorResult(ProcessResult result, Optional<File> file) {
        this.result = result;
        this.file = file;
    }


    public static ProcessorResult createApi(ProcessResult result, Optional<ResultResponse> response) {
        var res = new ProcessorResult(result, Optional.empty());
        res.setResponse(response);
        return res;
    }

    public static ProcessorResult createFile(ProcessResult result, Optional<File> file) {
        return new ProcessorResult(result, file);

    }


}
