package fr.bluechipit.dvdtheque.service.impl;

import java.util.List;
import java.util.Set;

import fr.bluechipit.dvdtheque.dao.domain.Personne;

public interface IPersonneService {
	public Personne findByPersonneId(Long id);
	public List<Personne> findAllPersonne();
	public Long savePersonne(Personne personne);
	public void updatePersonne(Personne p);
	public void deletePersonne(Personne p);
	public Personne findPersonneByName(String nom);
	public Personne getPersonne(Long id);
	public Personne loadPersonne(Long id);
	public void cleanAllPersonnes();
	public Personne createOrRetrievePersonne(String nom, String profilePath);
	public Personne buildPersonne(String nom, String profilePath);
	public String printPersonnes(final Set<Personne> personnes, final String separator);
	void cleanAllCaches();
	Personne attachSessionPersonneByName(String nom);
}
