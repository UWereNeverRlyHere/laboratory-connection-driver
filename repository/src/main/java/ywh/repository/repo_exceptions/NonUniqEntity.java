package ywh.repository.repo_exceptions;

public class NonUniqEntity extends IllegalArgumentException {
    public NonUniqEntity(String s) {super(s);}
}
