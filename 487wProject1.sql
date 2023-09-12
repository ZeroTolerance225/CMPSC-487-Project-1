-- Drop tables to re-run
IF OBJECT_ID('Swipes', 'U') IS NOT NULL 
    DROP TABLE Swipes;

IF OBJECT_ID('Users', 'U') IS NOT NULL 
    DROP TABLE Users;

IF OBJECT_ID('UserTypes', 'U') IS NOT NULL 
    DROP TABLE UserTypes;

-- UserTypes Table
-- This table holds the possible types a user can be categorized into.
CREATE TABLE [UserTypes] (
    [UserTypeId]  int IDENTITY(1,1) PRIMARY KEY,   
    [UserType]    nvarchar(30) NOT NULL            
);

-- Users Table
-- This table stores the personal information of each user.
CREATE TABLE [Users] (
    [UserId]      int IDENTITY(1,1) PRIMARY KEY,
    [UserTypeId]  int NOT NULL,
    [Name]        nvarchar(50) NOT NULL,
    [SchoolId]    nvarchar(9) UNIQUE NOT NULL, 
    [Status]      bit DEFAULT 1,  
    FOREIGN KEY ([UserTypeId]) REFERENCES [UserTypes]([UserTypeId]) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Swipes Table
-- This table captures swipe in/out activities for each user.
CREATE TABLE [Swipes] (
    [SwipeId]        int IDENTITY(1,1) PRIMARY KEY,
    [IsSwipeIn]      bit NOT NULL,                             
    [UserId]         int NOT NULL,
    [SwipeTimeStamp] DateTime NOT NULL,                        
    FOREIGN KEY ([UserId]) REFERENCES [Users]([UserId]) ON DELETE CASCADE ON UPDATE CASCADE
);

GO 

-- View to see swipe history
CREATE OR ALTER VIEW [SwipeHistory] AS
SELECT 
    swipe.*,
    us.[Name],
    us.[SchoolId]
FROM 
    [Swipes] AS swipe
    LEFT OUTER JOIN [Users] AS us ON us.[UserId] = swipe.[UserId];

GO 

-- Adding user types
INSERT INTO [UserTypes] ([UserType]) VALUES ('Student'), ('Faculty');

-- Inserting users
INSERT INTO [Users] ([UserTypeId], [Name], [SchoolId]) 
VALUES  (1, 'Student1', '123456789'), 
        (2, 'Faculty1', '987654321'),
        (1, 'Student2', '123456790'),
        (1, 'Student3', '123456791'),
        (2, 'Faculty2', '987654322'),
        (1, 'Student4', '123456792');

-- Swipe data for testing
INSERT INTO [Swipes] ([IsSwipeIn], [UserId], [SwipeTimeStamp])
VALUES  (1, 1, '2023-09-05 08:00:00'), -- swipe in
        (0, 1, '2023-09-05 10:00:00'), -- swipe out
        (1, 2, '2023-09-05 09:00:00'),
        (0, 2, '2023-09-05 12:00:00'),
        (1, 3, '2023-09-05 08:30:00'),
        (0, 3, '2023-09-05 11:00:00'),
        (1, 4, '2023-09-05 09:15:00'),
        (0, 4, '2023-09-05 15:00:00'),
        (1, 5, '2023-09-05 07:45:00'),
        (0, 5, '2023-09-05 09:45:00'),
        (1, 6, '2023-09-05 07:30:00'),
        (0, 6, '2023-09-05 10:30:00');
