module ywh.labs.repository {
    requires static lombok;
    requires com.google.gson;
    requires ywh.labs.commons;
    requires org.mapstruct;

    exports ywh.repository.analysis.mapping;
    exports ywh.repository.animals.enteties;
    exports ywh.repository.analysis.entities;
    exports ywh.repository.analysis.repos;
    exports ywh.repository.analysis.models;
    exports ywh.repository.repo_exceptions;


    opens ywh.repository.analysis.entities to com.google.gson;
    opens ywh.repository.animals.enteties to com.google.gson;
    opens ywh.repository.analysis.mapping to org.mapstruct;
    opens ywh.repository.analysis.models to com.google.gson;
    exports ywh.repository.analysis.repos.impl;

}