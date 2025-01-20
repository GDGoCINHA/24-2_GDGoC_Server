package inha.gdgoc.domain.recruit.entity;

import inha.gdgoc.domain.question.entity.DataType;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public abstract class AnswerData implements Serializable {
    private DataType type;

    public abstract void validate();
}
