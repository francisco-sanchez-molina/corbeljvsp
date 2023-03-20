package io.corbel.resources.rem.operation;

import org.im4java.core.IMOperation;
import org.im4java.core.IMOps;

import io.corbel.resources.rem.exception.ImageOperationsException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoOrient implements ImageOperation {

    private final Pattern pattern = Pattern.compile("^(true|false)$");
    @Override
    public IMOps apply(String parameter) throws ImageOperationsException {
        boolean active;
        try {
            Matcher matcher = pattern.matcher(parameter);

            if (!matcher.matches()) {
                throw new ImageOperationsException("Bad parameter autoOrient: " + parameter);
            }

            List<String> values = getValues(parameter, matcher);

            active = Boolean.parseBoolean(values.get(0));
        } catch (Exception e) {
            throw new ImageOperationsException("Bad parameter in autoOrient: " + parameter, e);
        }

        if (active) {
            return new IMOperation().autoOrient();
        } else {
            return new IMOperation();
        }
    }

    @Override
    public String getOperationName() {
        return "autoOrient";
    }

}
