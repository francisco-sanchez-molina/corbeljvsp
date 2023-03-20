package io.corbel.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.corbel.resources.ioc.ResourcesIoc;
import io.corbel.lib.cli.console.Console;
import io.corbel.lib.cli.console.Shell;
import io.corbel.lib.ws.log.LogbackUtils;

/**
 * @author Rubén Carrasco
 * 
 */
public class ResourcesConsoleRunner extends Console {

    public ResourcesConsoleRunner() {
        super("Welcome to SilkRoad Resources. Type resources.help() to start.", createShell());
    }

    @SuppressWarnings("resource")
    private static Map<String, Object> createShell() {
        String mode = Optional.ofNullable(System.getProperty("mode"))
                .orElse(Optional.ofNullable(System.getenv("MODE"))
                        .orElse("console_fast"));
        System.setProperty("mode", mode);
        System.setProperty("conf.namespace", "resources");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ResourcesIoc.class);
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Shell.class);
        Map<String, Object> shells = new HashMap<>(beans.size());
        beans.forEach((beanName, bean) -> {
            shells.put(applicationContext.findAnnotationOnBean(beanName, Shell.class).value(), bean);
        });
        return shells;

    }

    public static void main(String[] args) {
        LogbackUtils.setLogLevel("INFO");
        ResourcesConsoleRunner console = new ResourcesConsoleRunner();
        try {
            if (args.length == 0) {
                console.launch();
            } else {
                console.runScripts(args);
            }
            System.exit(0);
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

}
