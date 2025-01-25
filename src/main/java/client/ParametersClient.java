package client;

import com.beust.jcommander.Parameter;

final public class ParametersClient {
    @Parameter(names = "-in", description = "Text Request", required = false)
    private String textRequest = "";

    @Parameter(names = "-t", description = "Type", required = false)
    private String typeRequest = "";

    @Parameter(names = "-k", description = "Key", required = false)
    private String key = "";

    @Parameter(names = "-v", description = "Value", required = false)
    private String message = "";

    public String getTypeRequest() {
        return typeRequest;
    }

    public String getKey() {
        return key;
    }

    public String getMessage() {
        return message;
    }

    public String getTextRequest() {
        return textRequest;
    }
}
