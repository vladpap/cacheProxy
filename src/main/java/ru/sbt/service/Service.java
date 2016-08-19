package ru.sbt.service;

import java.io.Serializable;
import java.util.List;

public interface Service extends Serializable {
    double doHardWork(String work, int i);
    String doWorkEasy(String work);
    String doDo();
    List<String> work(String item);
}
