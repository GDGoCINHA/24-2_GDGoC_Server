package inha.gdgoc.domain.recruit.entity;

import inha.gdgoc.domain.question.enums.InputType;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public abstract class AnswerData implements Serializable {
    private InputType type;

    public abstract void validate();
}
