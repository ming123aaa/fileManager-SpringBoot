package com.example.filemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class FileManagerApplication {

    public static void main(String[] args) {
        if(args.length>=1){
            if (!args[0].isEmpty()) {
                ResPath.DownloadPath = args[0];
            }
        }
        File file = new File(ResPath.DownloadPath);
        if (!file.exists()){
            file.mkdirs();
        }
        SpringApplication.run(FileManagerApplication.class, args);
    }

}
