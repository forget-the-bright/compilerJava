package ${packageName};

import org.hao.core.print.PrintUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ${className} {

    public void run(){
        PrintUtil.BLUE.Println("Hello World!");
        log.info("name:{}",this.getClass().getName());
    }

}