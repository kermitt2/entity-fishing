package com.scienceminer.nerd.disambiguation;

import com.scienceminer.nerd.mention.ProcessText;
import org.grobid.core.analyzers.GrobidAnalyzer;

import java.util.*;
import java.util.function.Function;

public class PruningService {

    /**
     * We prune only entities overlapping and having the same disambiguation information
     */
    public List<NerdEntity> pruneOverlapNBest(List<NerdEntity> entities, boolean shortText) {
        Set<Integer> toRemove = new HashSet<>();
        for (int pos1 = 0; pos1 < entities.size(); pos1++) {
            if (toRemove.contains(pos1))
                continue;

            NerdEntity entity1 = entities.get(pos1);

            if (entity1.getRawName() == null) {
                toRemove.add(pos1);
                continue;
            }

            // find all sub term of this entity and entirely or partially overlapping entities
            for (int pos2 = 0; pos2 < entities.size(); pos2++) {
                if (pos1 == pos2)
                    continue;

                NerdEntity entity2 = entities.get(pos2);

                if (!areEntityOverlapping(entity1, entity2)) continue;

                if (toRemove.contains(pos2))
                    continue;

                // overlap
                if (entity2.getRawName() == null) {
                    toRemove.add(pos2);
                    continue;
                }

                if (entity1.getWikipediaExternalRef() == entity2.getWikipediaExternalRef()) {
                    if (entity1.getType() != null && entity2.getType() != null) {
                        if (entity1.getType().equals(entity2.getType())) {
                            toRemove.add(pos2);
                        }
                        continue;
                    } else if (entity1.getType() != null && entity2.getType() == null) {
                        toRemove.add(pos2);
                    } else {
                        if (entity1.getNerdScore() < entity2.getNerdScore()) {
                            toRemove.add(pos2);
                        }
                    }
                }
            }
        }

        List<NerdEntity> prunedEntities = new ArrayList<>();

        for (int i = 0; i < entities.size(); i++) {
            final NerdEntity currentEntity = entities.get(i);
            if (!toRemove.contains(i)) {
                prunedEntities.add(currentEntity);
            } else {
                if (shortText) {
                    currentEntity.setNerdScore(currentEntity.getNerdScore() / 2);
                    prunedEntities.add(currentEntity);
                }
            }
        }

        return prunedEntities;
    }

    public List<NerdEntity> prune(List<NerdEntity> entities, double threshold, Function<NerdEntity, Boolean> exclusionRule) {
        List<NerdEntity> toRemove = new ArrayList<>();

        for (NerdEntity entity : entities) {
            if (exclusionRule.apply(entity)) continue;

            // variant: prune named entities less aggressively
            if ((entity.getNerdScore() < threshold) && ((entity.getType() == null) || entity.getIsAcronym())) {
                toRemove.add(entity);
            } else if ((entity.getNerdScore() < threshold / 2) && (entity.getType() != null)) {
                toRemove.add(entity);
            }
        }

        List<NerdEntity> newEntities = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            if (!toRemove.contains(i)) {
                newEntities.add(entities.get(i));
            }
        }

        return newEntities;
    }

    public void prune(List<NerdEntity> entities, double threshold) {
        List<NerdEntity> toRemove = new ArrayList<>();

        for(NerdEntity entity : entities) {
            if (entity.getSource() == ProcessText.MentionMethod.species) {
                // dont prune such explicit mention recognition
                continue;
            }
			/*if (entity.getNerdScore() < threshold) {
				toRemove.add(entity);
			}*/
            // variant: prune named entities less aggressively
            if ( (entity.getNerdScore() < threshold) && ( (entity.getType() == null) || entity.getIsAcronym() ) ) {
                toRemove.add(entity);
            } else if ( (entity.getNerdScore() < threshold/2) && (entity.getType() != null) ) {
                toRemove.add(entity);
            }
        }

        for(NerdEntity entity : toRemove) {
            entities.remove(entity);
        }
    }
    
    public void prune(Map<NerdEntity, List<NerdCandidate>> candidates,
                      boolean nbest,
                      boolean shortText,
                      double threshold,
                      String lang) {
        List<NerdEntity> toRemove = new ArrayList<>();

        // we prune candidates for each entity mention
        for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
            List<NerdCandidate> cands = entry.getValue();
            if ( (cands == null) || (cands.size() == 0) )
                continue;
            NerdEntity entity = entry.getKey();
            List<NerdCandidate> newCandidates = new ArrayList<NerdCandidate>();
            for(NerdCandidate candidate : cands) {
                if (!nbest) {
                    if (shortText && (candidate.getNerdScore() > 0.10)) {
                        newCandidates.add(candidate);
                        break;
                    }
                    else if (candidate.getNerdScore() > threshold) {
                        newCandidates.add(candidate);
                        break;
                    }
                }
                else {
                    if (shortText && (candidate.getNerdScore() > 0.10)) {
                        newCandidates.add(candidate);
                    }
                    else if ( (newCandidates.size() == 0) && (candidate.getNerdScore() > threshold) ) {
                        newCandidates.add(candidate);
                    }
                    else if (candidate.getNerdScore() > 0.6) {
                        newCandidates.add(candidate);
                    }
                }
            }
            if (newCandidates.size() > 0)
                candidates.replace(entity, newCandidates);
            else {
                if (entity.getType() == null)
                    toRemove.add(entity);
                else
                    candidates.replace(entity, new ArrayList<NerdCandidate>());
            }
        }

        for(NerdEntity entity : toRemove) {
            candidates.remove(entity);
        }
    }


    /**
     * We prioritize the longest term match from the KB : the term coming from the KB shorter than
     * the longest match from the KB is pruned.
     * <p>
     * For equal mention arity, nerd confidence score is used.
     * Note that the longest match heuristics is debatable and should be further experimentally
     * validated...
     */
    public List<NerdEntity> pruneOverlap(List<NerdEntity> entities, boolean shortText) {

        Set<Integer> toRemove = new HashSet<>();
        for (int pos1 = 0; pos1 < entities.size(); pos1++) {
            if (toRemove.contains(pos1))
                continue;

            NerdEntity entity1 = entities.get(pos1);

            if (entity1.getRawName() == null || entity1.getNormalisedName() == null) {
                toRemove.add(pos1);
                continue;
            }

            GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance();

            // the arity measure below does not need to be precise
            int arity1 = analyzer.tokenize(entity1.getNormalisedName()).size();

            // find all sub term of this entity and entirely or partially overlapping entities
            for (int pos2 = 0; pos2 < entities.size(); pos2++) {
                if (pos1 == pos2)
                    continue;

                if (toRemove.contains(pos2))
                    continue;

                NerdEntity entity2 = entities.get(pos2);

                if (!areEntityOverlapping(entity1, entity2))
                    continue;

                // they overlap
                if (entity2.getRawName() == null) {
                    toRemove.add(pos2);
                    continue;
                }

                if (entity2.getType() != null && entity2.getWikipediaExternalRef() == -1) {
                    // we have a NER not disambiguated check if the other entity has been disambiguated
                    if (entity1.getWikipediaExternalRef() != -1 && entity1.getNerdScore() > 0.2) {
                        toRemove.add(pos2);
                        continue;
                    }
                }


                if (entity1.getWikipediaExternalRef() == entity2.getWikipediaExternalRef()) {
                    if (entity1.getType() != null && entity2.getType() == null) {
                        toRemove.add(pos2);
                        continue;
                    }
                }

                int arity2 = analyzer.tokenize(entity2.getNormalisedName()).size();
                if (arity2 < arity1) {
                    // longest match wins
                    toRemove.add(pos2);
                    continue;
                } else if (arity2 == arity1) {
                    // we check the nerd scores of the top candidate for the two entities
                    double conf1 = entity1.getNerdScore();
                    double conf2 = entity2.getNerdScore();
                    //double conf1 = entity1.getSelectionScore();
                    //double conf2 = entity2.getSelectionScore();
                    if (conf2 < conf1) {
                        toRemove.add(pos2);
                        continue;
                    } else {
                        double selectionConf1 = entity1.getSelectionScore();
                        double selectionConf2 = entity2.getSelectionScore();

                        if (selectionConf2 < selectionConf1) {
                            toRemove.add(pos2);
                        }
                    }
                }
            }
        }

        List<NerdEntity> newEntities = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            if (!toRemove.contains(i)) {
                newEntities.add(entities.get(i));
            } else {
                if (shortText) {
                    // in case of short text we simply reduce the score of the entity but we don't remove it
                    entities.get(i).setNerdScore(entities.get(i).getNerdScore() / 2);
                    newEntities.add(entities.get(i));
                }
            }
        }

        return newEntities;
    }

    public boolean areEntityOverlapping(NerdEntity entity1, NerdEntity entity2) {
        if (entity2.getOffsetEnd() < entity1.getOffsetStart())
            return false;

        if (entity1.getOffsetEnd() < entity2.getOffsetStart())
            return false;

        return true;
    }

    /**
     * 	We prioritize the longest term match from the KB : the term coming from the KB shorter than
     *  the longest match from the KB and which have not been merged, are lowered.
     */
    public void impactOverlap(Map<NerdEntity, List<NerdCandidate>> candidates) {
        //List<Integer> toRemove = new ArrayList<Integer>();

        for (Map.Entry<NerdEntity, List<NerdCandidate>> entry : candidates.entrySet()) {
            List<NerdCandidate> cands = entry.getValue();
            NerdEntity entity = entry.getKey();
            if (cands == null)
                continue;
            // the arity measure below does not need to be precise
            int arity = entity.getNormalisedName().split("[ ,-.]").length;
            for(NerdCandidate candidate : cands) {
                double score = candidate.getNerdScore();
                double new_score = score - ( (5-arity)*0.01);
                if ( (new_score > 0) && (new_score <= 1) )
                    candidate.setNerdScore(new_score);

            }
            Collections.sort(cands);
        }
    }
}
