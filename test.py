
def get_label(cat_index):
        categories = {1 : 'Wheel Marks', 2 : 'Longitudinal Construction Joint', 3 : 'Lateral Interval',
            4 : 'Lateral Construction Joint', 5 : 'Partial Pavement', 6 : 'Pothole', 7 : 'Cross Walk Blur', 8 : 'White Line Blur'}
        
        return categories[cat_index]

print(get_label(1))

