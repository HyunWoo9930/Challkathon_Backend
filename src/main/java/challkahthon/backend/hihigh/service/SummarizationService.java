package challkahthon.backend.hihigh.service;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SummarizationService {

    private final StanfordCoreNLP pipeline;
    
    public SummarizationService() {
        // Initialize Stanford CoreNLP pipeline
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        props.setProperty("coref.algorithm", "neural");
        
        try {
            this.pipeline = new StanfordCoreNLP(props);
        } catch (Exception e) {
            log.error("Error initializing Stanford CoreNLP: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize Stanford CoreNLP", e);
        }
    }
    
    /**
     * Summarize text using extractive summarization
     * @param text Text to summarize
     * @param maxSentences Maximum number of sentences in the summary
     * @return Summarized text
     */
    public String summarizeText(String text, int maxSentences) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        try {
            // Process the document
            CoreDocument document = new CoreDocument(text);
            pipeline.annotate(document);
            
            // Get sentences
            List<CoreSentence> sentences = document.sentences();
            
            // If the text is already short, return it as is
            if (sentences.size() <= maxSentences) {
                return text;
            }
            
            // Score sentences based on position and length
            List<ScoredSentence> scoredSentences = new ArrayList<>();
            for (int i = 0; i < sentences.size(); i++) {
                CoreSentence sentence = sentences.get(i);
                double positionScore = 1.0 - ((double) i / sentences.size()); // Earlier sentences get higher scores
                double lengthScore = Math.min(1.0, sentence.tokens().size() / 20.0); // Favor medium-length sentences
                double score = 0.6 * positionScore + 0.4 * lengthScore;
                
                scoredSentences.add(new ScoredSentence(sentence.text(), score, i));
            }
            
            // Sort by score and take top N sentences
            List<ScoredSentence> topSentences = scoredSentences.stream()
                    .sorted(Comparator.comparing(ScoredSentence::getScore).reversed())
                    .limit(maxSentences)
                    .collect(Collectors.toList());
            
            // Sort by original position to maintain flow
            topSentences.sort(Comparator.comparing(ScoredSentence::getPosition));
            
            // Join sentences
            return topSentences.stream()
                    .map(ScoredSentence::getText)
                    .collect(Collectors.joining(" "));
            
        } catch (Exception e) {
            log.error("Error summarizing text: {}", e.getMessage());
            // Return a truncated version of the original text if summarization fails
            return text.length() > 500 ? text.substring(0, 500) + "..." : text;
        }
    }
    
    /**
     * Helper class to store sentence with its score and original position
     */
    private static class ScoredSentence {
        private final String text;
        private final double score;
        private final int position;
        
        public ScoredSentence(String text, double score, int position) {
            this.text = text;
            this.score = score;
            this.position = position;
        }
        
        public String getText() {
            return text;
        }
        
        public double getScore() {
            return score;
        }
        
        public int getPosition() {
            return position;
        }
    }
    
    /**
     * Summarize text with default maximum of 5 sentences
     * @param text Text to summarize
     * @return Summarized text
     */
    public String summarizeText(String text) {
        return summarizeText(text, 5);
    }
}