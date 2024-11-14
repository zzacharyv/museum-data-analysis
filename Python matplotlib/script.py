import mysql.connector
import matplotlib.pyplot as plt


def main():
    mydb = mysql.connector.connect(
        host="localhost",
        user="user",
        password="password",
        database="test"
    )
    cursor = mydb.cursor()
    cursor.execute("SELECT * FROM Landscapes")

    date_end = []

    for i in cursor:
        date = i[3]
        if date != None and date >=0:
            date_end.append(i[3])
    
    num_bins = 200
    
    plt.hist(date_end, num_bins, color ='green')
    
    
    plt.xlabel('Year Artwork Completed')
    plt.ylabel('Frequency')
    
    plt.title("Distribution of Landscape Artworks by Year Completed",
            fontweight = "bold")
    plt.savefig('histogram.png')
    plt.show()

if __name__ == "__main__":
    main()
