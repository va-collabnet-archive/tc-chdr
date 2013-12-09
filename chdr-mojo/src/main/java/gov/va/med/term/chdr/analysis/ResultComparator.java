package gov.va.med.term.chdr.analysis;

import java.util.Comparator;

public class ResultComparator implements Comparator<String[]>
{
	private AlphanumComparator ac;
	
	public ResultComparator()
	{
		ac = new AlphanumComparator(true);
	}
	
	@Override
	public int compare(String[] o1, String[] o2)
	{
		//vhat concept id
		int r = ac.compare(o1[3], o2[3]);
		if (r == 0)
		{
			//vhat description id
			r = ac.compare(o1[0], o2[0]);
		}
		if (r == 0)
		{
			//current mapping id
			r = ac.compare(o1[4], o2[4]);
		}
		return r;
	}

}
