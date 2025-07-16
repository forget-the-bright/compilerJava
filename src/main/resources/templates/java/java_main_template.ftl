${packageName}

import org.hao.core.print.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ${className} {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        PrintUtil.BLUE.Println("Hello World!");
        log.info("name:{}",this.getClass().getName());
    }

}