package ru.sbt.service;

import java.io.Serializable;

public interface Service extends Serializable {
    double doHardWork(String work, int i);
    String doWorkEasy(String work);
    String doDo();
}
