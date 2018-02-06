package com.scienceminer.nerd.disambiguation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    /**
     * We prioritize the longest term match from the KB : the term coming from the KB shorter than
     * the longest match from the KB is pruned.
     * <p>
     * For equal mention arity, nerd confidence score is used.
     * Note that the longest match heuristics is debatable and should be further experimentally
     * validated...
     */
    public List<NerdEntity> pruneOverlap(List<NerdEntity> entities, boolean shortText) {
        //System.out.println("pruning overlaps - we have " + entities.size() + " entities");
        Set<Integer> toRemove = new HashSet<>();
        for (int pos1 = 0; pos1 < entities.size(); pos1++) {
            if (toRemove.contains(pos1))
                continue;

            NerdEntity entity1 = entities.get(pos1);

            if (entity1.getRawName() == null || entity1.getNormalisedName() == null) {
                toRemove.add(pos1);
                //System.out.println("Removing " + pos1 + " - " + entity1.getNormalisedName());
                continue;
            }

            // the arity measure below does not need to be precise
            int arity1 = entity1.getNormalisedName().length() - entity1.getNormalisedName().replaceAll("\\s", "").length() + 1;
            //System.out.println("Position1 " + pos1 + " / arity1 : " + entity1.getNormalisedName() + ": " + arity1);

            // find all sub term of this entity and entirely or partially overlapping entities
            for (int pos2 = 0; pos2 < entities.size(); pos2++) {
                if (pos1 == pos2)
                    continue;

                if (toRemove.contains(pos2))
                    continue;

                NerdEntity entity2 = entities.get(pos2);

                if (!areEntityOverlapping(entity1, entity2))
                    continue;

                // overlap
                //int arity2 = entity2.getOffsetEnd() - entity2.getOffsetStart();
                if (entity2.getRawName() == null) {
                    toRemove.add(pos2);
                    continue;
                }

                if ((entity2.getType() != null) && (entity2.getWikipediaExternalRef() == -1)) {
                    // we have a NER not disambiguated
                    // check if the other entity has been disambiguated
                    if ((entity1.getWikipediaExternalRef() != -1) && (entity1.getNerdScore() > 0.2)) {
                        toRemove.add(pos2);
                        continue;
                    }
                }

                if ((entity1.getType() != null) && (entity1.getWikipediaExternalRef() == -1)) {
                    // we have a NER not disambiguated
                    // check if the other entity has been disambiguated
                    if ((entity2.getWikipediaExternalRef() != -1) && (entity2.getNerdScore() > 0.2)) {
                        toRemove.add(pos1);
                        break;
                    }
                }


                if (entity1.getWikipediaExternalRef() == entity2.getWikipediaExternalRef()) {
                    if ((entity1.getType() != null) && (entity2.getType() == null)) {
                        toRemove.add(pos2);
                        continue;
                    }
                }

                int arity2 = entity2.getNormalisedName().length() - entity2.getNormalisedName().replaceAll("\\s", "").length() + 1;
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
                    }
                    /*else {
							// if equal we check the selection scores of the top candiate for the two entities
							conf1 = entity1.getSelectionScore();
							conf2 = entity2.getSelectionScore();
							if (conf2 < conf1) {
								if (!toRemove.contains(new Integer(pos2))) {
									toRemove.add(new Integer(pos2));
								}
							} else {
								// if still equal we check the prob_c
								conf1 = entity1.getProb_c();
								conf2 = entity2.getProb_c();
								if (conf2 < conf1) {
									if (!toRemove.contains(new Integer(pos2))) {
										toRemove.add(new Integer(pos2));
									}
								} else {
									// too uncertain we remove all
									if (!toRemove.contains(new Integer(pos2))) {
										toRemove.add(new Integer(pos2));
									}
									if (!toRemove.contains(new Integer(pos1))) {
										toRemove.add(new Integer(pos1));
                    }
                }
							}
						}*/

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
}
