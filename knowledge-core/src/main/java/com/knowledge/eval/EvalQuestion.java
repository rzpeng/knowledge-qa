package com.knowledge.eval;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvalQuestion {
    private String id;
    private String question;
    private List<Long> relevantDocumentIds;
    private String expectedAnswer;
    private String category;
}
