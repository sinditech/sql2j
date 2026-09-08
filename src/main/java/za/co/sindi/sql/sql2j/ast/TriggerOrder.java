package za.co.sindi.sql.sql2j.ast;

/** MySQL-only: {@code FOLLOWS other_trigger} / {@code PRECEDES other_trigger}, controlling firing order among same-event triggers. */
public record TriggerOrder(TriggerOrderPosition position, String otherTriggerName) {}
