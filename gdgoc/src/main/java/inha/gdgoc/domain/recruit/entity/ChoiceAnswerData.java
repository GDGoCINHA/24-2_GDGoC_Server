package inha.gdgoc.domain.recruit.entity;

import java.util.List;

public class ChoiceAnswerData extends AnswerData {
    private List<String> selectedOptions;

    @Override
    public void validate() {
        // Answer에 접근하지 않고 Question의 DataType에 따라 검증할 수 있는 로직 필요
    }
}