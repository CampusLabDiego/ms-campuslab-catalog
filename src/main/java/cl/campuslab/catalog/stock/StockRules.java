package cl.campuslab.catalog.stock;

/**
 * Reglas de inventario independientes de HTTP
 * y de la base de datos.
 */
public final class StockRules {

    private StockRules() {
    }

    public enum Action {
        RESERVE,
        RELEASE,
        FINISH
    }

    public enum State {
        RESERVED,
        RELEASED,
        RETURNED,
        CONSUMED
    }

    public record Result(
            int total,
            int available,
            State state,
            boolean consumable
    ) {
    }

    public static Result apply(
            Integer total,
            Integer available,
            int quantity,
            Action action,
            State previous,
            boolean wasConsumable,
            boolean isSupply,
            boolean active,
            boolean laboratory
    ) {

        if (
                total == null
                || available == null
                || total < 0
                || available < 0
                || available > total
                || quantity < 1
        ) {
            throw new IllegalArgumentException(
                    "Revisa el stock total y disponible del recurso."
            );
        }

        if (previous == null) {

            if (action == Action.RELEASE) {
                // Cancelar sin descuento previo no agrega unidades.
                return new Result(
                        total,
                        available,
                        State.RELEASED,
                        isSupply
                );
            }

            if (action == Action.FINISH) {
                throw new IllegalArgumentException(
                        "Esta reserva no tiene descuento previo. "
                        + "Sincroniza su stock antes de devolverla."
                );
            }

            if (!active) {
                throw new IllegalArgumentException(
                        "El recurso está inactivo."
                );
            }

            if (laboratory && quantity != 1) {
                throw new IllegalArgumentException(
                        "Un laboratorio se reserva con cantidad 1."
                );
            }

            if (quantity > available) {
                throw new IllegalArgumentException(
                        "No hay stock disponible suficiente para aprobar."
                );
            }

            return new Result(
                    total,
                    available - quantity,
                    State.RESERVED,
                    isSupply
            );
        }

        // Repetir una reserva no vuelve a descontar.
        if (
                action == Action.RESERVE
                && previous == State.RESERVED
        ) {
            return new Result(
                    total,
                    available,
                    previous,
                    wasConsumable
            );
        }

        // Repetir una cancelación no vuelve a devolver.
        if (
                action == Action.RELEASE
                && previous == State.RELEASED
        ) {
            return new Result(
                    total,
                    available,
                    previous,
                    wasConsumable
            );
        }

        // Repetir el cierre conserva el saldo.
        if (
                action == Action.FINISH
                && (
                        previous == State.RETURNED
                        || previous == State.CONSUMED
                )
        ) {
            return new Result(
                    total,
                    available,
                    previous,
                    wasConsumable
            );
        }

        if (
                previous != State.RESERVED
                || action == Action.RESERVE
        ) {
            throw new IllegalArgumentException(
                    "El movimiento de stock ya está cerrado "
                    + "para esta reserva."
            );
        }

        if (action == Action.FINISH && wasConsumable) {

            if (total - quantity < available) {
                throw new IllegalArgumentException(
                        "El inventario de insumos necesita revisión."
                );
            }

            return new Result(
                    total - quantity,
                    available,
                    State.CONSUMED,
                    true
            );
        }

        long restored = (long) available + quantity;

        if (restored > total) {
            throw new IllegalArgumentException(
                    "La devolución superaría el stock total."
            );
        }

        return new Result(
                total,
                (int) restored,
                action == Action.RELEASE
                        ? State.RELEASED
                        : State.RETURNED,
                wasConsumable
        );
    }
}