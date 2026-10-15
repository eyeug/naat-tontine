package sn.naat.model;

/**
 * Membre &lt;|-- Gerant.
 * Un gérant est toujours aussi membre (il participe à sa propre
 * tontine). Il porte les opérations de gestion d'une tontine
 * précise, appliquées via les services (GerantService), avec
 * les paramètres typés sur le bon sous-type de Tontine comme
 * dans le diagramme de classes.
 */
public class Gerant extends Membre {

    public Gerant() {
        super();
    }

    public Gerant(Utilisateur u) {
        super(u);
    }
}
